create table process (id varchar(40) not null, active boolean not null, created timestamp not null, instance_id bigint not null, parameters jsonb, session_id bigint not null, workflow_id varchar(255) not null, primary key (id));
create table task (id varchar(40) not null, actions jsonb, comment text, completed timestamp, created timestamp not null, name varchar(255), parameters jsonb, result jsonb, status varchar(255) not null, task_id varchar(255) not null, updated timestamp, user_id varchar(255), user_role varchar(255), work_item_id bigint not null, fk_process varchar(40) not null, primary key (id));
create index ix_task_fk_task_fk_process on task (fk_process);
create index ix_task_work_item_id on task (work_item_id);
create index ix_task_user_id on task (user_id);
create index ix_task_user_role on task (user_role);
create index ix_task_parameters on task using gin (parameters);
create index ix_process_parameters on process using gin (parameters);
alter table task add constraint fk_task_fk_process foreign key (fk_process) references process (id);