create table public.uuid_table
(
	id     uuid not null primary key,
	random numeric
);

create table public.varchar_table
(
	id            varchar(40) not null primary key,
	random        numeric,
	fk_uuid_table uuid references uuid_table (id)
);

create table public.uuid_table_varchar_table
(
	fk_uuid_table    uuid references uuid_table (id),
	fk_varchar_table varchar(40) references varchar_table (id)
)
