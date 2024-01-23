create table customer
(
	id         varchar(40) not null primary key,
	age        integer,
	first_name varchar(255)
);

create table product
(
	id   varchar(40) not null primary key,
	name varchar(255)
);

create table "order"
(
	fk_customer varchar(40) not null references customer (id),
	fk_order    varchar(40) not null references product (id),

	primary key (fk_customer, fk_order)
);

create index on "order"(fk_customer);
create index on "order"(fk_order);
