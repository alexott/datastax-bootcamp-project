// Inventory-related
use atwaters_inventory;

drop materialized view if exists inventory_by_base_sku;
drop materialized view if exists inventory_by_upc;
drop table if exists inventory;

//

drop type if exists item_meta;
create type if not exists item_meta (
   name text, // it should be also mapped into user-readable name by UI
   type int, // 0 - text, 1 - int, 2 - double
   value_t text,
   value_i int,
   value_f double
);

// Initial idea was to group common information about item with different sizes, etc. into one partition, and share it between all sub-SKUs via static columns
// But static columns aren't supported by Search
create table if not exists inventory (
  base_sku uuid,  // base SKU for family of products - could be shared by multiple items
  upc text,   // universal product code - unique
  sku uuid,   // sku for this model - unique
  available boolean, // true if it's available (could be used for keeping the historical items)
  title text, // title for an item
  metainfo frozen<set<item_meta>>,
  urls frozen<list<text>>, // item-specific URLs
  tags frozen<set<text>>, // item-specific tags
  rating float, // rating for specific item
  rating_count int, // number of ratings...
  country text, // ISO 3166-1 alpha-3: https://en.wikipedia.org/wiki/ISO_3166-1_alpha-3
  description text, // description of an item
  currency text, // ISO 4217 code: https://en.wikipedia.org/wiki/ISO_4217
  price decimal,
  buy_price decimal,
  
  primary key(sku, country, upc)
);

// create search index. TODO: adjust field types
CREATE SEARCH INDEX ON atwaters_inventory.inventory WITH COLUMNS upc, available, tags, price, rating, description, country, metainfo, title, rating_count
    AND OPTIONS { reindex : false };
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.inventory ADD types.fieldtype[@class='org.apache.solr.schema.TextField', @name='TextField']
//WITH '{"analyzer":{"class":"org.apache.lucene.analysis.standard.StandardAnalyzer"}}';
WITH '{"analyzer":{"tokenizer":{"class":"solr.StandardTokenizerFactory"}, "filter":[{"class": "solr.StandardFilterFactory"}, {"class": "solr.LowerCaseFilterFactory"},{"class": "solr.StopFilterFactory"}]}}';
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.inventory SET field[@name='title']@type='TextField';
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.inventory SET field[@name='description']@type='TextField';
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.inventory ADD fields.field[@name='metainfo_text', @type='TextField', @indexed='true'];
ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.inventory ADD copyField[@source='metainfo.value_t', @dest='metainfo_text'];

// TODO: tweak softCommit & some other parameters...

// create suggestion component & corresponding request handler.
// due the DSE limitations, it's impossible to create suggester on the UDT's field, or copyField.
ALTER SEARCH INDEX CONFIG ON atwaters_inventory.inventory ADD searchComponent[@name='suggest',@class='solr.SuggestComponent'] 
WITH  $$ {"suggester":[{"name":"titleSuggester"}, {"lookupImpl":"AnalyzingInfixLookupFactory"}, {"dictionaryImpl":"DocumentDictionaryFactory"},
{"suggestAnalyzerFieldType":"TextField"}, {"field":"title"}, {"weightField":"rating"}, {"buildOnCommit":"false"}, {"exactMatchFirst":"true"},
{"contextField":"country"}]} $$;
ALTER SEARCH INDEX CONFIG ON atwaters_inventory.inventory ADD requestHandler[@name='/suggest',@class='solr.SearchHandler'] 
WITH  $$ {"defaults":[{"suggest":"true"}, {"suggest.count":"10"}],"components":["suggest"]} $$;

// reload config & rebuild it
RELOAD SEARCH INDEX ON atwaters_inventory.inventory;
//REBUILD SEARCH INDEX ON atwaters_inventory.inventory;

// for reverse lookup by UPC & BASE SKU
create materialized view inventory_by_upc as select base_sku, sku, upc, country from inventory 
   where upc is not null and sku is not null and country is not null
   primary key (upc, sku, country);
create materialized view inventory_by_base_sku as select base_sku, sku, upc, country from inventory 
   where upc is not null and sku is not null and country is not null and base_sku is not null
   primary key (base_sku, sku, country, upc);

// 
// insert into inventory (base_sku, sku, upc, available, tags, urls, rating, title, description, country, price, buy_price, currency)
// values (f5c03dd1-2e78-11e8-8d2e-eb69c2afd03b, f5c03dd1-2e78-11e8-8d2e-eb69c2afd03b, '123456789012', true, {'books'}, 
// ['https://images-na.ssl-images-amazon.com/images/I/41HEHU5%2BNgL._SX329_BO1,204,203,200_.jpg'], 
// 4.0, 
// 'Introduction to Reliable and Secure Distributed Programming, 2ed',
// 'Cachin, Guerraoui, and Rodrigues present an introductory description of fundamental distributed programming abstractions together with algorithms to implement them in distributed systems, where processes are subject to crashes and malicious attacks.',
// 'USA', 58.17, 50, 'USD'
// );

// insert into inventory (base_sku, sku, upc, available, tags, urls, rating, title, description, country, price, buy_price, currency)
// values (0f65a401-2e79-11e8-8d2e-eb69c2afd03b, 0f65a401-2e79-11e8-8d2e-eb69c2afd03b, '456123456789', true, {'electronics'}, 
// ['https://images-na.ssl-images-amazon.com/images/I/711giY9YufL._SL1500_.jpg'], 
// 1.0, 'Super loud speaker!', '', 'USA', 62, 50, 'USD'
// );

// insert into inventory (base_sku, sku, upc, available, tags, urls,  rating, title, description, country, price, buy_price, currency)
// values (15b93561-2e79-11e8-8d2e-eb69c2afd03b, 15b93561-2e79-11e8-8d2e-eb69c2afd03b, '661234567890', true, {'clothes'}, 
// ['https://images-na.ssl-images-amazon.com/images/I/81pRjgs-yuL._SL1500_.jpg'], 
// 4.9, 'Sunglasses Polarized Women', '', 'USA', 200, 150, 'USD'
// );

// insert into inventory (base_sku, sku, upc, available, tags, urls, rating, title, description, country, price, buy_price, currency)
// values (1bdc40e2-2e79-11e8-8d2e-eb69c2afd03b, 1bdc40e2-2e79-11e8-8d2e-eb69c2afd03b, '551234567890', true, {'books'}, 
// ['https://images.gr-assets.com/books/1451247412l/28328882.jpg'], 
// 4.0, 'Relevant Search: With applications for Solr and Elasticsearch', '', 'USA', 42, 35, 'USD'
// );

COMMIT SEARCH INDEX ON atwaters_inventory.inventory;

// select * from inventory where solr_query='{!tuple v=''price.location:USA AND price.price:150''}';

// TODO: do we need to have an additional search field with all text data?
//ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.inventory ADD fields.field[@name='search_all', @type='TextField', @multiValued='true'];
//ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.inventory ADD copyField[@source='info.title', @dest='search_all'];
//ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.inventory ADD copyField[@source='info.description', @dest='search_all'];
//ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.inventory ADD copyField[@source='metainfo.value_t', @dest='search_all'];

// TODO: think when rebuild suggestor's dictionaries? Trigger manually after changing data, or on soft commits, or something else?
// TODO: think about other options or lookupImpl (such as Fuzzy search): see https://lucene.apache.org/solr/guide/6_6/suggester.html
//ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.inventory ADD fields.field[@name='suggestor', @type='TextField', @multiValued='true', @stored='true'];
//ALTER SEARCH INDEX SCHEMA ON atwaters_inventory.inventory ADD copyField[@source='info.title', @dest='suggestor'];

