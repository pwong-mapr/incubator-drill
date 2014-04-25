DROP TABLE IF EXISTS KV;
DROP TABLE IF EXISTS dest4_sequencefile;
create table kv (key string, value string) stored as textfile;
load data local inpath 'resources/hive_storage/datasources/kv1.txt' into table kv;

CREATE TABLE dest4_sequencefile(key INT, value STRING) STORED AS SEQUENCEFILE;

FROM kv
INSERT OVERWRITE TABLE dest4_sequencefile SELECT kv.key, kv.value;