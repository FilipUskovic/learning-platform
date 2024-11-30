
alter table if exists courses
add column author_id UUID,
add column category varchar(100),
add column difficulty_level varchar(50),
add column estimated_duration interval,
add column max_students integer default 100;


-- indexi
create index idx_course_category_level on courses (category, difficulty_level);

-- modul tablica

create table if not exists course_modules
(
    id UUID primary key ,
    course_id UUID not null ,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    sequence_number INTEGER NOT NULL,
    duration INTERVAL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,

    constraint fk_module_course foreign key (course_id) references courses(id)
        on delete cascade,
    constraint uk_module_sequence unique (course_id, sequence_number)

);

CREATE INDEX idx_module_course ON course_modules(course_id);
