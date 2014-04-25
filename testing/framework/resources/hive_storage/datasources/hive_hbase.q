DROP TABLE IF EXISTS pokes;
CREATE TABLE pokes (foo INT, bar STRING);
DROP TABLE IF EXISTS hbase_table_1;

load data local inpath 'resources/hive_storage/datasources/kv1.txt' OVERWRITE INTO TABLE pokes;

CREATE TABLE hbase_table_1(key int, value string)
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES ("hbase.columns.mapping" = ":key,cf1:val")
TBLPROPERTIES ("hbase.table.name" = "xyz");

INSERT OVERWRITE TABLE hbase_table_1 SELECT * FROM pokes WHERE foo=98;
