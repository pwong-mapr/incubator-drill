#!/bin/bash
###############################################################################
# Generic Script to Track Resources
# DEPENDANCY: watchCPUMem.sh; NetMeter; IOStat; VMStat; mapr-guts/MPStat
# TIMELINE:
# Kunal Khatua [Jan 31, 2014] - Support for DrillBit tracking
###############################################################################

if [ $# -lt 1 ]; then
	echo "[ERROR] Insufficient Arguments!"
	echo "Usage: $0 <uniqID>"
	exit 127
fi

##### Parameters
uniqID=$1

# Defaults (RemoteServer - NFS Mount)
ReportServer=kk127
RemoteDumpDir=/root/kunal/zliuFW/org.apache.drill.test/drilltests/reports
# Defaults (Sampling Interval)
interval=1
# Defaults (Process Tracking Script)
procTracker=watchCPUMem.sh
# Defaults (DumpDir)
#[2DO] How to decide this? Location of this script?
LocalDumpDir=/root/kunal/zliuFW/org.apache.drill.test/drilltests/reports/
# Defaults (WorkDir)
#[2DO] How to decide this? Location of this script?
WorkDir=$LocalDumpDir
#Java Location
JAVA_EXE=`which java`
# Guts Options
GUTS_OPTS="cpu:all db:none cache:all fs:all rpc:all log:none"
# NetMeter Directory
NetMeterDir=/root/kunal/zliuFW/org.apache.drill.test/drilltests/NetMeter

### Switching To Directory
ReportsDir=$LocalDumpDir/$uniqID
mkdir -p $ReportsDir
# Defaults (Process Tracking Script)
killerScript=$WorkDir/killTrackers.sh
cd $ReportsDir
rm -rf nohup.out

### Identifying Cluster Type
isMapRCluster=`md5sum /opt/mapr/bin/guts | awk '{print $1}'`
if [ -n $isMapRCluster ]; then 
	echo "MapR Cluster detected!"
else 
	echo "HDFS Cluster detected!"
fi


####### System Statistics
#--- VMStat ---#
VMSTAT_FILE=$ReportsDir/vmstat-${uniqID}.log
echo "Starting vmstat"
nohup /usr/bin/vmstat $interval | awk '{now=strftime("%Y-%m-%d %T "); print now $0}' &> $VMSTAT_FILE &
VMSTAT_PID=$!
echo "kill -p $VMSTAT_PID" >> $killerScript
echo "killall vmstat" >> $killerScript
#echo "echo Killing $VMSTAT_PID " >> $killerScript
#--------------#

#--- IOStat ---#
IOSTAT_FILE=$ReportsDir/iostat-${uniqID}.log
echo "Starting iostat"
nohup /usr/bin/iostat -dxm $interval | awk '{now=strftime("%Y-%m-%d %T "); print now $0}' &> $IOSTAT_FILE &
IOSTAT_PID=$!
echo "kill -9 $IOSTAT_PID" >> $killerScript
#--------------#

#--- Network ---#
NETMETER_FILE=$ReportsDir/netmeter-${uniqID}.log
echo "Starting NetMeter"
$JAVA_EXE -Djava.library.path=${NetMeterDir}/lib -jar ${NetMeterDir}/NetMeterTS.jar 1 &> $NETMETER_FILE &
NETMETER_PID=$!
echo "kill -9 "$NETMETER_PID >> $killerScript
#--------------#

#--- CPU / Guts ---#
if [ -z $isMapRCluster ]; then 
	CPU_FILE=${ReportsDir}/cpu-${uniqID}.log
	echo "Starting mpstat" 
	nohup /usr/bin/mpstat -P ALL $interval 99999999 > $cpuUsageFile &
	MPSTAT_PID=$!
	echo "kill -9 $MPSTAT_PID" >> $killerScript
else
	GUTS_FILE=$ReportsDir/guts-${uniqID}.log
	echo "Starting guts" 
	nohup /opt/mapr/bin/guts time:all flush:line ${GUTS_OPTS} &> $GUTS_FILE &
	GUTS_PID=$!
	echo "kill -9 $GUTS_PID" >> $killerScript
fi
#--------------#
####### /System Statistics 

####### Process Statistics 
#--- MFS/HDFS ---#
if [ -n $isMapRCluster ]; then 
	DFS_PROCESS=mfs
	DFS_FILTER=
else
	DFS_PROCESS=hdfs
	DFS_FILTER=DataNode
fi
echo "Tracking $DFS_PROCESS"
nohup $WorkDir/$procTracker $uniqID $interval -k $DFS_PROCESS $DFS_FILTER &
DFSSTAT_PID=$!
echo "kill -9 $DFSSTAT_PID" >> $killerScript
#--------------#

#--- DrillBit ---#
echo "Tracking DrillBit"
nohup $WorkDir/$procTracker $uniqID $interval -k DrillBit drill.exec.server &
DRILLBIT_PID=$!
echo "kill -9 $DRILLBIT_PID" >> $killerScript
#--------------#
####### /Process Statistics 


### Finalizing Script
#[2Do] GZip Files! 
# Moving
echo "_scp -R $ReportsDir ${ReportServer}:${RemoteDumpDir}" >> $killerScript
echo "echo \"Moving Files to ${ReportServer}:${RemoteDumpDir}\"; sleep 3" >> $killerScript
# Deleting self
echo "rm -rf \$0" >> $killerScript
chmod 755 $killerScript
