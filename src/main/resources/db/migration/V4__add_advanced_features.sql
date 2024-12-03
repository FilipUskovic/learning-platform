--podrška za full text search
alter table if exists courses add column search_vector tsvector;

-- funkcija koja će automatski ažurirati search vector

CREATE FUNCTION courses_search_vector_trigger() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
            setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
            setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Kreiramo trigger koji će održavati search vector
 create trigger courses_search_vector_update before
    insert or update on courses for each row
    execute function courses_search_vector_trigger();

-- gist  indeks za brzo full text pretraživanje
CREATE INDEX courses_search_idx ON courses USING gin(search_vector);

