create table result_file (
   id bigint not null,
   path varchar(255) not null,
   submission_id bigint not null,
   mime_type varchar(255) not null,
   content text,
   content_binary bytea,
   primary key (id)
);

alter table result_file
  add constraint either_binary_or_not check (
    (content is not null and content_binary is null) or
    (content is null and content_binary is not null)
);

create sequence result_file_seq start with 1 increment by 50;

alter table result_file add constraint FKfiecoo6Nu7gaidengae1chae2 foreign key (submission_id) references submission;

alter table task
  add column persistent_result_file_paths JSON;

update task set persistent_result_file_paths = '[]'::json;

alter table task
  alter column persistent_result_file_paths set not null;

