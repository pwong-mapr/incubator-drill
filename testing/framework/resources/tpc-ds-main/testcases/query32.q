select * from (select  sum(cs_ext_discount_amt)  as "excess discount amount" 
from 
   catalog_sales 
   ,item 
   ,date_dim
where
i_manufact_id = 291
and i_item_sk = cs_item_sk 
and d_date between to_date('2000-03-22', 'YYYY-MM-DD') and 
        (to_date('2000-03-22','YYYY-MM-DD') + interval '90' day)
and d_date_sk = cs_sold_date_sk 
and cs_ext_discount_amt  
     > ( 
         select 
            1.3 * avg(cs_ext_discount_amt) 
         from 
            catalog_sales 
           ,date_dim
         where 
              cs_item_sk = i_item_sk 
          and d_date between to_date('2000-03-22','YYYY-MM-DD') and
                             (to_date('2000-03-22', 'YYYY-MM-DD') + interval '90' day)
          and d_date_sk = cs_sold_date_sk 
      ) 
) where rownum <= 100;
