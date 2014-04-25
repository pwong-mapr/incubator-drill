DROP TABLE IF EXISTS KV;
DROP TABLE IF EXISTS dest1;
create table kv (key string, value string) stored as textfile;
load data local inpath 'resources/hive_storage/datasources/kv1.txt' into table kv;

CREATE TABLE dest1(key INT, value STRING) STORED AS
INPUTFORMAT 'org.apache.hadoop.mapred.SequenceFileInputFormat'
OUTPUTFORMAT 'org.apache.hadoop.mapred.SequenceFileOutputFormat';
  
FROM kv
INSERT OVERWRITE TABLE dest1 SELECT kv.key, kv.value WHERE kv.key < 10;