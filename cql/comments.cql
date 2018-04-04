use atwaters_inventory;

// comments
drop table if exists comments;
create table comments (
  base_sku uuid,
  sku uuid,
  user_id uuid,
  posted timestamp,
  user_name text,
  comment text,
  rating int,
  
  primary key(base_sku, posted, user_id, sku) // should we allow multiple postings by the same user?
) with clustering order by (posted desc);



// insert into comments(base_sku, sku, user_id, posted, user_name, comment, rating) 
// values (f5c03dd1-2e78-11e8-8d2e-eb69c2afd03b, f5c03dd1-2e78-11e8-8d2e-eb69c2afd03b, 6a23b0f0-2e77-11e8-8d2e-eb69c2afd03b, 
// '2018-03-13T09:31:51.511+0000', 'user 1', 'That is very good product!', 4);

// insert into comments(base_sku, sku, user_id, posted, user_name, comment, rating) 
// values (0f65a401-2e79-11e8-8d2e-eb69c2afd03b, 0f65a401-2e79-11e8-8d2e-eb69c2afd03b, 8c0a47b3-2e77-11e8-8d2e-eb69c2afd03b, 
// '2018-03-19T09:31:51.507+0000', 'user 3', 'That is very bad product!', 1);

// insert into comments(base_sku, sku, user_id, posted, user_name, comment, rating) 
// values (15b93561-2e79-11e8-8d2e-eb69c2afd03b, 15b93561-2e79-11e8-8d2e-eb69c2afd03b, 9287a832-2e77-11e8-8d2e-eb69c2afd03b, 
// '2018-03-21T09:31:51.514+0000', 'user 4', 'That is beatiful product!', 5);

// insert into comments(base_sku, sku, user_id, posted, user_name, comment, rating) 
// values (1bdc40e2-2e79-11e8-8d2e-eb69c2afd03b, 1bdc40e2-2e79-11e8-8d2e-eb69c2afd03b, 99945062-2e77-11e8-8d2e-eb69c2afd03b, 
// '2018-03-20T09:31:51.509+0000', 'user 2', 'The quality is good', 4);

// user 1: 6a23b0f0-2e77-11e8-8d2e-eb69c2afd03b
// user 2: 99945062-2e77-11e8-8d2e-eb69c2afd03b
// user 3: 8c0a47b3-2e77-11e8-8d2e-eb69c2afd03b
// user 4: 9287a832-2e77-11e8-8d2e-eb69c2afd03b

