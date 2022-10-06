drop index ix_task_user_role;
alter table task rename column user_role to user_roles;
alter table task alter column user_roles type text[] using array_append('{}', user_roles);
alter table task add column candidate_user_ids text[] default '{}';

create index ix_task_user_roles on task(user_roles);
create index ix_task_candidate_user_ids on task(candidate_user_ids);