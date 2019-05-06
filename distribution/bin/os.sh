#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

export PATH=$PATH:/sbin

#来源说明：http://soliloquize.org/2018/09/02/RocketMQ-ossh%E5%8F%82%E6%95%B0%E8%AF%B4%E6%98%8E/
#vm.extra_free_kbytes这个参数应该也是用来控制空闲内存大小的。但是很多发行版没有这个参数，
#一般根据部署环境看是否支持，不支持的话不进行设置应该也没关系，有时需要配合vm.min_free_kbytes
#使用，使用不当会导致：Cache占用过多内存导致系统内存不足问题
# sudo sysctl -w vm.extra_free_kbytes=2000000

#vm.min_free_kbytes设置系统需要保留的最小内存大小。当系统内存小于该数值时，则不再进行内存分配。
#这个命令默认被注释掉，根据文档看，如果设置了不恰当的值，比如比实际内存大，则系统可能直接就会崩溃掉。
#实际数值应该根据RocketMQ部署机器的内存进行计算，经验数值大概是机器内存的5% - 10%。
#这个数值设置的过高，则内存浪费。若设置的过低，那么在内存消耗将近时，
#RocketMQ的Page Cache写入操作可能会很慢，导致服务不可用。
# sudo sysctl -w vm.min_free_kbytes=1000000

#vm.overcommit_memory控制是否允许内存overcommit。设为1，则是允许。当应用申请内存时，系统都会认为存在足够的内存，准许申请。
#参考：linux的vm.overcommit_memory的内存分配参数详解.md
sudo sysctl -w vm.overcommit_memory=1
sudo sysctl -w vm.drop_caches=1
sudo sysctl -w vm.zone_reclaim_mode=0
sudo sysctl -w vm.max_map_count=655360
sudo sysctl -w vm.dirty_background_ratio=50
sudo sysctl -w vm.dirty_ratio=50
sudo sysctl -w vm.dirty_writeback_centisecs=360000
sudo sysctl -w vm.page-cluster=3
sudo sysctl -w vm.swappiness=1

echo 'ulimit -n 655350' >> /etc/profile
echo '* hard nofile 655350' >> /etc/security/limits.conf

echo '* hard memlock      unlimited' >> /etc/security/limits.conf
echo '* soft memlock      unlimited' >> /etc/security/limits.conf

DISK=`df -k | sort -n -r -k 2 | awk -F/ 'NR==1 {gsub(/[0-9].*/,"",$3); print $3}'`
[ "$DISK" = 'cciss' ] && DISK='cciss!c0d0'
echo 'deadline' > /sys/block/${DISK}/queue/scheduler


echo "---------------------------------------------------------------"
sysctl vm.extra_free_kbytes
sysctl vm.min_free_kbytes
sysctl vm.overcommit_memory
sysctl vm.drop_caches
sysctl vm.zone_reclaim_mode
sysctl vm.max_map_count
sysctl vm.dirty_background_ratio
sysctl vm.dirty_ratio
sysctl vm.dirty_writeback_centisecs
sysctl vm.page-cluster
sysctl vm.swappiness

su - admin -c 'ulimit -n'
cat /sys/block/$DISK/queue/scheduler

if [ -d ${HOME}/tmpfs ] ; then
    echo "tmpfs exist, do nothing."
else
    ln -s /dev/shm ${HOME}/tmpfs
    echo "create tmpfs ok"
fi
