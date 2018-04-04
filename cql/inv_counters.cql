use atwaters_inventory;

drop table if exists inv_counters;
create table inv_counters (
   sku uuid,
   shop uuid,
   cnt counter,

   primary key (sku, shop)
);

// UPDATE inv_counters SET cnt = cnt + 10 WHERE sku = af9e0ba1-2cea-11e8-bf30-ad0d88dfcd0f AND shop = 053d1800-2735-11e8-ad05-81d2afcf1ee5;
// UPDATE inv_counters SET cnt = cnt + 1 WHERE sku = af9d4851-2cea-11e8-bf30-ad0d88dfcd0f AND shop = 053d1800-2735-11e8-ad05-81d2afcf1ee5;
// UPDATE inv_counters SET cnt = cnt + 5 WHERE sku = af9d9674-2cea-11e8-bf30-ad0d88dfcd0f AND shop = 053d1800-2735-11e8-ad05-81d2afcf1ee5;
// UPDATE inv_counters SET cnt = cnt + 20 WHERE sku = af9d4851-2cea-11e8-bf30-ad0d88dfcd0f AND shop = 053d1800-2735-11e8-ad05-81d2afcf1ee5;

// UPDATE inv_counters SET cnt = cnt + 1 WHERE sku = af9e0ba1-2cea-11e8-bf30-ad0d88dfcd0f AND shop = f2a6cb00-2734-11e8-ad05-81d2afcf1ee5;
// UPDATE inv_counters SET cnt = cnt + 1 WHERE sku = af9d4851-2cea-11e8-bf30-ad0d88dfcd0f AND shop = f2a6cb00-2734-11e8-ad05-81d2afcf1ee5;
// UPDATE inv_counters SET cnt = cnt + 5 WHERE sku = af9d9674-2cea-11e8-bf30-ad0d88dfcd0f AND shop = f2a6cb00-2734-11e8-ad05-81d2afcf1ee5;
// UPDATE inv_counters SET cnt = cnt + 2 WHERE sku = af9d4851-2cea-11e8-bf30-ad0d88dfcd0f AND shop = f2a6cb00-2734-11e8-ad05-81d2afcf1ee5;

// UPDATE inv_counters SET cnt = cnt + 1 WHERE sku = af9e0ba1-2cea-11e8-bf30-ad0d88dfcd0f AND shop = 0bef9240-2735-11e8-ad05-81d2afcf1ee5;
// UPDATE inv_counters SET cnt = cnt + 1 WHERE sku = af9d4851-2cea-11e8-bf30-ad0d88dfcd0f AND shop = 0bef9240-2735-11e8-ad05-81d2afcf1ee5;
// UPDATE inv_counters SET cnt = cnt + 1 WHERE sku = af9d9674-2cea-11e8-bf30-ad0d88dfcd0f AND shop = 0bef9240-2735-11e8-ad05-81d2afcf1ee5;
// UPDATE inv_counters SET cnt = cnt + 1 WHERE sku = af9d4851-2cea-11e8-bf30-ad0d88dfcd0f AND shop = 0bef9240-2735-11e8-ad05-81d2afcf1ee5;
