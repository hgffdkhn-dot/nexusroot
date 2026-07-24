use std::collections::HashMap;
use std::os::unix::io::{FromRawFd, IntoRawFd};
use std::os::unix::net::UnixListener as StdUnixListener;
use std::sync::Arc;
use tokio::net::UnixListener;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::sync::Mutex;
use prost::Message;
use socket2::{Socket, Domain, Type, SockAddr};

// 包含通过 build.rs 生成的 protobuf 代码
mod pb {
    include!(concat!(env!("OUT_DIR"), "/nexusroot.rs"));
}

use pb::*;

// 简单的白名单存储
struct AppState {
    whitelist: HashMap<i32, WhitelistItem>, // key = uid
}

impl AppState {
    fn new() -> Self {
        Self { whitelist: HashMap::new() }
    }

    fn handle_request(&self, req: Request) -> Response {
        match req.payload {
            Some(request::Payload::Status(_)) => {
                Response {
                    success: true,
                    payload: Some(response::Payload::Status(StatusResponse {
                        daemon_alive: true,
                        su_version: "NexusRoot v1.0.0".into(),
                        su_path: "/data/adb/nxr/bin/nr-su".into(),
                        se_context: "u:r:nxr_daemon:s0".into(),
                    })),
                }
            }
            Some(request::Payload::Whitelist(wl_req)) => {
                match WhitelistRequestAction::try_from(wl_req.action).unwrap() {
                    WhitelistRequestAction::List => {
                        let items: Vec<WhitelistItem> = self.whitelist.values().cloned().collect();
                        Response {
                            success: true,
                            payload: Some(response::Payload::Whitelist(WhitelistResponse { items })),
                        }
                    }
                    WhitelistRequestAction::Add => {
                        // 实际代码中应修改状态，这里演示回显
                        Response {
                            success: true,
                            payload: Some(response::Payload::Whitelist(WhitelistResponse {
                                items: wl_req.items,
                            })),
                        }
                    }
                    WhitelistRequestAction::Remove => {
                        Response {
                            success: true,
                            payload: Some(response::Payload::Whitelist(WhitelistResponse {
                                items: wl_req.items,
                            })),
                        }
                    }
                }
            }
            _ => Response { success: false, payload: None },
        }
    }
}

// 提取枚举映射
#[derive(Debug, PartialEq)]
enum WhitelistRequestAction {
    List = 0,
    Add = 1,
    Remove = 2,
}

impl TryFrom<i32> for WhitelistRequestAction {
    type Error = ();
    fn try_from(v: i32) -> Result<Self, ()> {
        match v {
            0 => Ok(WhitelistRequestAction::List),
            1 => Ok(WhitelistRequestAction::Add),
            2 => Ok(WhitelistRequestAction::Remove),
            _ => Err(()),
        }
    }
}

async fn handle_client(mut stream: tokio::net::UnixStream, state: Arc<Mutex<AppState>>) {
    let mut buf = vec![0u8; 4096];
    loop {
        match stream.read(&mut buf).await {
            Ok(0) => break, // 连接关闭
            Ok(n) => {
                if let Ok(req) = Request::decode(&buf[..n]) {
                    let state = state.lock().await;
                    let resp = state.handle_request(req);
                    let mut resp_buf = Vec::new();
                    resp.encode(&mut resp_buf).unwrap();
                    if let Err(e) = stream.write_all(&resp_buf).await {
                        eprintln!("Failed to write response: {}", e);
                        break;
                    }
                }
            }
            Err(e) => {
                eprintln!("Read error: {}", e);
                break;
            }
        }
    }
}

/// 使用 socket2 创建抽象 Unix 域 socket
fn create_abstract_listener(name: &str) -> StdUnixListener {
    let socket = Socket::new(Domain::UNIX, Type::STREAM, None)
        .expect("Failed to create socket");
    // 抽象地址：前导 null 字节 + 名称
    let addr = SockAddr::unix(format!("\0{}", name))
        .expect("Failed to create abstract address");
    socket.bind(&addr).expect("Failed to bind abstract socket");
    socket.listen(128).expect("Failed to listen");
    // 转换为标准库的 UnixListener
    let fd = socket.into_raw_fd();
    unsafe { StdUnixListener::from_raw_fd(fd) }
}

#[tokio::main]
async fn main() {
    let socket_name = "nxr_daemon";  // 抽象 socket 名，不包括 @

    let std_listener = create_abstract_listener(socket_name);
    std_listener.set_nonblocking(true).expect("Failed to set nonblocking");
    let listener = UnixListener::from_std(std_listener).expect("Failed to create tokio listener");

    println!("nexusrootd listening on @{}", socket_name);

    let state = Arc::new(Mutex::new(AppState::new()));

    loop {
        match listener.accept().await {
            Ok((stream, _)) => {
                println!("New client connected");
                let state = Arc::clone(&state);
                tokio::spawn(handle_client(stream, state));
            }
            Err(e) => eprintln!("Accept error: {}", e),
        }
    }
}
