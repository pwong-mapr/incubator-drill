set hive.map.aggr.hash.percentmemory = 0.3;
set hive.mapred.local.mem = 256;

DROP TABLE IF EXISTS KV;
DROP TABLE IF EXISTS columnTable_Bigdata;
create table kv (key string, value string) stored as textfile;
load data local inpath 'resources/hive_storage/datasources/kv1.txt' into table kv;

add file resources/hive_storage/datasources/dumpdata_script.py;

CREATE table columnTable_Bigdata (key STRING, value STRING)
ROW FORMAT SERDE
  'org.apache.hadoop.hive.serde2.columnar.ColumnarSerDe'
STORED AS
  INPUTFORMAT 'org.apache.hadoop.hive.ql.io.RCFileInputFormat'
  OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.RCFileOutputFormat';

FROM (FROM kv MAP kv.key,kv.value USING 'python dumpdata_script.py' AS (key,value) WHERE kv.key = 10) subq
INSERT OVERWRITE TABLE columnTable_Bigdata SELECT subq.key, subq.value;

describe columnTable_Bigdata;
