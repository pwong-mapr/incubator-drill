DROP TABLE IF EXISTS ALLTYPES;
DROP TABLE IF EXISTS ALLDRILLTYPES;
create table alltypes
         (c1 int, c2 boolean, c3 double, c4 string,
         c5 array<int>, c6 map<int,string>, c7 map<string,string>,
         c8 struct<r:string,s:int,t:double>,
         c9 tinyint, c10 smallint, c11 float, c12 bigint,
         c13 array<array<string>>,
         c14 map<int, map<int,int>>,
         c15 struct<r:int,s:struct<a:int,b:string>>,
         c16 array<struct<m:map<string,string>,n:int>>,
         c17 timestamp,
         c18 decimal,
         c19 binary,
         c20 date);
         
load data local inpath 'resources/hive_storage/datasources/datatypes.txt' into table alltypes;
         
create table alldrilltypes 
         (c1 int, c2 boolean, c3 double, c4 string,
         c9 tinyint, c10 smallint, c11 float, c12 bigint,
         c19 binary);

FROM alltypes
INSERT OVERWRITE TABLE alldrilltypes SELECT c1,c2,c3,c4,c9,c10,c11,c12,c19;         

         

         

