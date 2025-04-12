alter table process add column engine_type varchar(255) not null default 'JBPM';
alter table process alter column instance_id type varchar using instance_id::varchar;

alter table task alter column work_item_id type varchar using work_item_id::varchar;