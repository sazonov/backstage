create table process_event (id varchar(40) not null, fk_process varchar(40) not null, event varchar(255) not null, primary key (id));
alter table process_event add constraint fk_process_event_fk_process foreign key (fk_process) references process (id);
