#!/bin/bash
# 运行脚本 - 自动包含所有Maven依赖

cd "$(dirname "$0")"

# 方法1: 使用Maven运行（推荐）
echo "=== 使用Maven运行 ==="
mvn compile exec:java -Dexec.mainClass="com.jd.jvm.day02.ObjectCreationDemo"

# 方法2: 如果想用java -cp运行，需要先用Maven复制依赖
# mvn dependency:copy-dependencies -DoutputDirectory=target/lib
# java -cp "target/classes:target/lib/*" com.jd.jvm.day02.ObjectCreationDemo
