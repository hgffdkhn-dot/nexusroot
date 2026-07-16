#!/system/bin/sh
# 设备检测脚本 - 输出 JSON 形式的信息

echo "{"
echo "  \"ro.build.version.sdk\": \"$(getprop ro.build.version.sdk)\","
echo "  \"ro.build.version.release\": \"$(getprop ro.build.version.release)\","

# A/B 分区检测
ab_slot=$(getprop ro.boot.slot_suffix)
if [ -n "$ab_slot" ]; then
    echo "  \"ab_partition\": true,"
    echo "  \"current_slot\": \"$ab_slot\","
else
    echo "  \"ab_partition\": false,"
fi

# System-as-root 检测（Android 9+ 强制启用）
if [ -f /system/init ] || [ -L /system ] ; then
    # SAR 的标志：/system 是符号链接或根目录没有 init
    echo "  \"system_as_root\": true,"
else
    echo "  \"system_as_root\": false,"
fi

# 动态分区检测
if getprop ro.boot.dynamic_partitions | grep -q "true"; then
    echo "  \"dynamic_partitions\": true,"
else
    echo "  \"dynamic_partitions\": false,"
fi

# 内核版本
kernel=$(uname -r)
echo "  \"kernel_version\": \"$kernel\","

# GKI 内核检测（5.4 以上通常为 GKI）
if echo "$kernel" | grep -qE "^[5-9]\.|^4\.1[4-9]|^4\.[2-9][0-9]"; then
    echo "  \"likely_gki\": true"
else
    echo "  \"likely_gki\": false"
fi

echo "}"
