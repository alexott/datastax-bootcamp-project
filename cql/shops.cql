use atwaters_inventory;

drop table if exists shops;

create table shops (
  id uuid,
  url text,
  location 'PointType',
  address  text,
  city     text,
  state    text,
  zip      text,
  country  text,
  phone frozen<set<text>>,
  fax   frozen<set<text>>,
  email frozen<set<text>>,
  primary key(id)
);

// search index
CREATE SEARCH INDEX IF NOT EXISTS ON atwaters_inventory.shops WITH COLUMNS zip, country, state, city, address, id, location 
AND OPTIONS { reindex : false, lenient: true};
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.shops ADD types.fieldtype[@class='solr.TextField', @name='IStrField']
WITH '{"analyzer":{"tokenizer":{"class":"solr.KeywordTokenizerFactory"}, "filter":{"class": "solr.LowerCaseFilterFactory"}}}';
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.shops ADD types.fieldtype[@class='solr.SpatialRecursivePrefixTreeFieldType', @name='PointField'];
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.shops SET field[@name='location']@type='PointField';


// create a copy of address fields for case-insensitive search. Original fields will be used for faceting 
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.shops ADD fields.field[@name='zip_s', @type='IStrField', @indexed='true'];
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.shops ADD copyField[@source='zip', @dest='zip_s'];
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.shops ADD fields.field[@name='state_s', @type='IStrField', @indexed='true'];
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.shops ADD copyField[@source='state', @dest='state_s'];
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.shops ADD fields.field[@name='country_s', @type='IStrField', @indexed='true'];
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.shops ADD copyField[@source='country', @dest='country_s'];
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.shops ADD fields.field[@name='city_s', @type='IStrField', @indexed='true'];
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.shops ADD copyField[@source='city', @dest='city_s'];
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.shops ADD fields.field[@name='address_s', @type='IStrField', @indexed='true'];
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.shops ADD copyField[@source='address', @dest='address_s'];

RELOAD SEARCH INDEX ON atwaters_inventory.shops;

// REBUILD SEARCH INDEX ON atwaters_inventory.shops;
// DROP SEARCH INDEX ON atwaters_inventory.shops;
// data

// insert into shops (id, url, address, city, state, zip, country, phone)
//   values(f2a6cb00-2734-11e8-ad05-81d2afcf1ee5, 'http://atwaters.com/us/shop1',
//   'Shopping street, 10', 'Santa Clara', 'CA', '1234', 'US', {'+1 123 456789'});

// US web shop
// insert into shops (id, url, country, phone)
//   values(053d1800-2735-11e8-ad05-81d2afcf1ee5, 'http://atwaters.com',
//   'US', {'+1 123 456789'});

// DE web shop
// insert into shops (id, url, country, phone)
//   values(0bef9240-2735-11e8-ad05-81d2afcf1ee5, 'http://atwaters.de',
//   'DE', {'+49 123 456789'});

// 7c512800-273a-11e8-ad05-81d2afcf1ee5
// 818ce0c0-273a-11e8-ad05-81d2afcf1ee5
// 12f9f081-2735-11e8-ad05-81d2afcf1ee5
// 87e246e1-273a-11e8-ad05-81d2afcf1ee5
// 
//ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.shops ADD types.fieldtype[@class='org.apache.solr.schema.TextField', @name='TextField']
// WITH '{"analyzer":{"tokenizer":{"class":"solr.StandardTokenizerFactory"}, "filter":{"class": "solr.LowerCaseFilterFactory"}}}';

