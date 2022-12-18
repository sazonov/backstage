drop index ix_task_fk_task_fk_process;
create index if not exists ix_task_fk_process on task using btree (fk_process) include (type, status);