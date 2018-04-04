use atwaters_usa;

drop table if exists cart;

create table cart(
  user_id uuid, // keyed by user ID (not referenced here)
  items_count int static,
  updated timestamp static,
  sku uuid,
  title text, // title for an item
  imgurl text, // URL of image. URL of product itself is generated from SKU
  currency text,
  price double,
  
  primary key(user_id, sku)
);

// insert into cart(user_id, items_count, sku, title, imgurl, currency, price) values (6a23b0f0-2e77-11e8-8d2e-eb69c2afd03b, 1, f5c03dd1-2e78-11e8-8d2e-eb69c2afd03b,
// 'Introduction to Reliable and Secure Distributed Programming, 2ed', 'https://images-na.ssl-images-amazon.com/images/I/41HEHU5%2BNgL._SX329_BO1,204,203,200_.jpg',
// 'USD', 58.17);

// insert into cart(user_id, items_count, sku, title, imgurl, currency, price) values (6a23b0f0-2e77-11e8-8d2e-eb69c2afd03b, 2, 1bdc40e2-2e79-11e8-8d2e-eb69c2afd03b,
// 'Relevant Search: With applications for Solr and Elasticsearch', 'https://images.gr-assets.com/books/1451247412l/28328882.jpg', 'USD', 42);

// insert into cart(user_id, items_count, sku, title, imgurl, currency, price) values (9287a832-2e77-11e8-8d2e-eb69c2afd03b, 1, 15b93561-2e79-11e8-8d2e-eb69c2afd03b,
// 'Sunglasses Polarized Women', 'https://images-na.ssl-images-amazon.com/images/I/81pRjgs-yuL._SL1500_.jpg', 'USD', 200);
