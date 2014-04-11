select * from (select  i_item_id
       ,i_item_desc
       ,i_current_price
 from item, inventory, date_dim, store_sales
 where i_current_price between 38 and 38+30
 and inv_item_sk = i_item_sk
 and d_date_sk=inv_date_sk
 and d_date between to_date('1998-01-06','YYYY-MM-DD') and (to_date('1998-01-06','YYYY-MM-DD') +  interval '60' day)
 and i_manufact_id in (198,999,168,196)
 and inv_quantity_on_hand between 100 and 500
 and ss_item_sk = i_item_sk
 group by i_item_id,i_item_desc,i_current_price
 order by i_item_id
 ) where rownum <= 100;
