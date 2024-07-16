create table assignment (
  id bigint not null,
  enabled boolean not null,
  end_date timestamp(6),
  ordinal_num integer not null,
  slug varchar(255) not null,
  start_date timestamp(6),
  course_id bigint not null,
  primary key (id));

create table assignment_information (
  id bigint not null,
  language varchar(255) not null,
  title varchar(255) not null,
  assignment_id bigint not null,
  primary key (id));

create table course (
  id bigint not null,
  default_visibility varchar(255) not null check (
    default_visibility in ('HIDDEN','REGISTERED','PUBLIC')),
  logo varchar(255),
  override_end timestamp(6),
  override_start timestamp(6),
  override_visibility varchar(255) check (
    override_visibility in ('HIDDEN','REGISTERED','PUBLIC')),
  repository varchar(255) not null,
  slug varchar(255) not null,
  student_role varchar(255) not null,
  primary key (id));

create table course_assistants (
  course_id bigint not null,
  assistants varchar(255));

create table course_supervisors (
  course_id bigint not null,
  supervisors varchar(255));

create table course_information (
  id bigint not null,
  description varchar(255) not null,
  language varchar(255) not null,
  period varchar(255) not null,
  title varchar(255) not null,
  university varchar(255) not null,
  course_id bigint not null,
  primary key (id));

create table evaluation (
  id bigint not null,
  best_score float(53),
  remaining_attempts integer,
  user_id varchar(255) not null,
  task_id bigint not null,
  primary key (id));

create table evaluator (
  id bigint not null,
  docker_image varchar(255) not null,
  grade_command varchar(255) not null,
  run_command varchar(255) not null,
  test_command varchar(255),
  time_limit integer not null,
  primary key (id));

create table event (
  id bigint not null,
  date timestamp(6) not null,
  description varchar(255) not null,
  type varchar(255),
  course_id bigint not null,
  primary key (id));

create table submission (
  id bigint not null,
  command varchar(255) not null check (
    command in ('RUN','TEST','GRADE')),
  created_at timestamp(6) not null,
  logs text,
  ordinal_num bigint not null,
  output text,
  points float(53),
  user_id varchar(255) not null,
  valid boolean not null,
  evaluation_id bigint not null,
  primary key (id));

create table submission_file (
  id bigint not null,
  content text not null,
  submission_id bigint not null,
  task_file_id bigint not null,
  primary key (id));

create table task (
  id bigint not null,
  attempt_window numeric(21,0),
  docker_image varchar(255) not null,
  grade_command varchar(255) not null,
  max_attempts integer not null,
  max_points float(53) not null,
  ordinal_num integer not null,
  run_command varchar(255) not null,
  slug varchar(255) not null,
  test_command varchar(255),
  time_limit integer not null,
  assignment_id bigint not null,
  primary key (id));

create table task_file (
  id bigint not null,
  is_binary boolean not null,
  editable boolean not null,
  enabled boolean not null,
  grading boolean not null,
  instruction boolean not null,
  language varchar(255) not null,
  name varchar(255) not null,
  path varchar(255) not null,
  solution boolean not null,
  template text not null,
  visible boolean not null,
  task_id bigint not null,
  primary key (id));

create table task_information (
  id bigint not null,
  instructions_file varchar(255) not null,
  language varchar(255) not null,
  title varchar(255) not null,
  task_id bigint not null,
  primary key (id));

alter table if exists course add constraint UK_dduji3l55k63yvfcxs7n07bba unique (slug);

create sequence assignment_information_seq start with 1 increment by 50;

create sequence assignment_seq start with 1 increment by 50;

create sequence course_information_seq start with 1 increment by 50;

create sequence course_seq start with 1 increment by 50;

create sequence evaluation_seq start with 1 increment by 50;

create sequence evaluator_seq start with 1 increment by 50;

create sequence event_seq start with 1 increment by 50;

create sequence submission_file_seq start with 1 increment by 50;

create sequence submission_seq start with 1 increment by 50;

create sequence task_file_seq start with 1 increment by 50;

create sequence task_information_seq start with 1 increment by 50;

create sequence task_seq start with 1 increment by 50;

alter table if exists assignment add constraint FKrop26uwnbkstbtfha3ormxp85 foreign key (course_id) references course;

alter table if exists assignment_information add constraint FK7ui6vcqo38urjv2s8qvkikn4e foreign key (assignment_id) references assignment;

alter table if exists course_assistants add constraint FK51lfshow4447i1golpq2sopgr foreign key (course_id) references course;

alter table if exists course_supervisors add constraint FKox0twngr0b9gijefbefi5p8wr foreign key (course_id) references course;

alter table if exists course_information add constraint FKsy1fiq0mm5id77ww5811ni9xl foreign key (course_id) references course;

alter table if exists evaluation add constraint FKbbqpi9l6p6x5g13193g09cphf foreign key (task_id) references task;

alter table if exists event add constraint FKftnwq8qoxcybj9xrn8313vwli foreign key (course_id) references course;

alter table if exists submission add constraint FKd56a04x38qd5x2s6pfk7coh6f foreign key (evaluation_id) references evaluation;

alter table if exists submission_file add constraint FKtl06hyqfb54pe4evyq6omcb0n foreign key (submission_id) references submission;

alter table if exists submission_file add constraint FK8q5c4pww9mjmgeopmfeh3nibt foreign key (task_file_id) references task_file;

alter table if exists task add constraint FK875lan8jdhkua2w8s9y8hsbun foreign key (assignment_id) references assignment;

alter table if exists task_file add constraint FKk9ikv3hs4cyrgi4ti09b02px0 foreign key (task_id) references task;

alter table if exists task_information add constraint FKfq1pi7h983c8o6yluwa1exiak foreign key (task_id) references task;

