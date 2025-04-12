create table ticket
(
	id				varchar(40) 	not null primary key,
	created			timestamp 		not null default current_timestamp,
	title 			varchar(100)
);

create table ticket_information
(
	fk_ticket		varchar(40) 	not null references ticket (id),
	description		varchar(500),
	sender_email	varchar(100) 	not null
);

create index on ticket_information(fk_ticket);

insert into ticket (id, title) values ('ticket_id', 'ticket_title');
insert into ticket_information (fk_ticket, description, sender_email) values ('ticket_id', 'description', 'email');
