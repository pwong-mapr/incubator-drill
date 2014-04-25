select * from (select  i_item_id
       ,i_item_desc
       ,i_current_price
 from item, inventory, date_dim, catalog_sales
 where i_current_price between 42 and 42 + 30
 and inv_item_sk = i_item_sk
 and d_date_sk=inv_date_sk
 and d_date between to_date('2002-01-18','YYYY-MM-DD') and (to_date('2002-01-18','YYYY-MM-DD') +  interval '60' day)
 and i_manufact_id in (744,691,853,946)
 and inv_quantity_on_hand between 100 and 500
 and cs_item_sk = i_item_sk
 group by i_item_id,i_item_desc,i_current_price
 order by i_item_id
 ) where rownum <= 100;
