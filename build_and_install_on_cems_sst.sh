git pull github master
mvn clean install package assembly:directory

rm -rf /group_workspaces/cems2/esacci_sst/mms_new/bin/*
cp -r target/fiduceo-master-1.1.1-SNAPSHOT-MMS/* /group_workspaces/cems2/esacci_sst/mms_new/bin