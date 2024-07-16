alter table task
  add column enabled boolean;

update task set enabled = true;

alter table task
  alter column enabled set not null;
