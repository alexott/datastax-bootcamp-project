create keyspace if not exists atwaters_inventory with replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
create keyspace if not exists atwaters_usa with replication = {'class': 'SimpleStrategy', 'replication_factor': 1}; // should I have per country?
