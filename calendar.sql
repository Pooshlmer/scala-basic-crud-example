--
-- PostgreSQL database dump
--

-- Dumped from database version 9.3.5
-- Dumped by pg_dump version 9.3.5
-- Started on 2014-12-15 08:20:42

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- TOC entry 177 (class 3079 OID 11750)
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- TOC entry 1971 (class 0 OID 0)
-- Dependencies: 177
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 170 (class 1259 OID 24637)
-- Name: accountuser; Type: TABLE; Schema: public; Owner: testapp_role; Tablespace: 
--

CREATE TABLE accountuser (
    id integer NOT NULL,
    email text,
    username text,
    password text,
    role text,
    timezone integer
);


ALTER TABLE public.accountuser OWNER TO testapp_role;

--
-- TOC entry 171 (class 1259 OID 24643)
-- Name: event; Type: TABLE; Schema: public; Owner: testapp_role; Tablespace: 
--

CREATE TABLE event (
    id integer NOT NULL,
    title text,
    stream_link text,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    owner text,
    deleted boolean
);


ALTER TABLE public.event OWNER TO testapp_role;

--
-- TOC entry 172 (class 1259 OID 24649)
-- Name: event_game_xref; Type: TABLE; Schema: public; Owner: testapp_role; Tablespace: 
--

CREATE TABLE event_game_xref (
    event_id integer NOT NULL,
    game_id integer NOT NULL,
    tier integer
);


ALTER TABLE public.event_game_xref OWNER TO testapp_role;

--
-- TOC entry 173 (class 1259 OID 24652)
-- Name: event_id_seq; Type: SEQUENCE; Schema: public; Owner: testapp_role
--

CREATE SEQUENCE event_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.event_id_seq OWNER TO testapp_role;

--
-- TOC entry 1972 (class 0 OID 0)
-- Dependencies: 173
-- Name: event_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: testapp_role
--

ALTER SEQUENCE event_id_seq OWNED BY event.id;


--
-- TOC entry 174 (class 1259 OID 24654)
-- Name: game; Type: TABLE; Schema: public; Owner: testapp_role; Tablespace: 
--

CREATE TABLE game (
    id integer NOT NULL,
    title text
);


ALTER TABLE public.game OWNER TO testapp_role;

--
-- TOC entry 175 (class 1259 OID 24660)
-- Name: games_id_seq; Type: SEQUENCE; Schema: public; Owner: testapp_role
--

CREATE SEQUENCE games_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.games_id_seq OWNER TO testapp_role;

--
-- TOC entry 1973 (class 0 OID 0)
-- Dependencies: 175
-- Name: games_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: testapp_role
--

ALTER SEQUENCE games_id_seq OWNED BY game.id;


--
-- TOC entry 176 (class 1259 OID 24662)
-- Name: user_id_seq; Type: SEQUENCE; Schema: public; Owner: testapp_role
--

CREATE SEQUENCE user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.user_id_seq OWNER TO testapp_role;

--
-- TOC entry 1974 (class 0 OID 0)
-- Dependencies: 176
-- Name: user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: testapp_role
--

ALTER SEQUENCE user_id_seq OWNED BY accountuser.id;


--
-- TOC entry 1843 (class 2604 OID 24664)
-- Name: id; Type: DEFAULT; Schema: public; Owner: testapp_role
--

ALTER TABLE ONLY accountuser ALTER COLUMN id SET DEFAULT nextval('user_id_seq'::regclass);


--
-- TOC entry 1844 (class 2604 OID 24665)
-- Name: id; Type: DEFAULT; Schema: public; Owner: testapp_role
--

ALTER TABLE ONLY event ALTER COLUMN id SET DEFAULT nextval('event_id_seq'::regclass);


--
-- TOC entry 1845 (class 2604 OID 24666)
-- Name: id; Type: DEFAULT; Schema: public; Owner: testapp_role
--

ALTER TABLE ONLY game ALTER COLUMN id SET DEFAULT nextval('games_id_seq'::regclass);


--
-- TOC entry 1853 (class 2606 OID 24668)
-- Name: event_game_xref_pkey; Type: CONSTRAINT; Schema: public; Owner: testapp_role; Tablespace: 
--

ALTER TABLE ONLY event_game_xref
    ADD CONSTRAINT event_game_xref_pkey PRIMARY KEY (event_id, game_id);


--
-- TOC entry 1855 (class 2606 OID 24670)
-- Name: games_pkey; Type: CONSTRAINT; Schema: public; Owner: testapp_role; Tablespace: 
--

ALTER TABLE ONLY game
    ADD CONSTRAINT games_pkey PRIMARY KEY (id);


--
-- TOC entry 1857 (class 2606 OID 24672)
-- Name: games_title_key; Type: CONSTRAINT; Schema: public; Owner: testapp_role; Tablespace: 
--

ALTER TABLE ONLY game
    ADD CONSTRAINT games_title_key UNIQUE (title);


--
-- TOC entry 1851 (class 2606 OID 24674)
-- Name: id_pkey; Type: CONSTRAINT; Schema: public; Owner: testapp_role; Tablespace: 
--

ALTER TABLE ONLY event
    ADD CONSTRAINT id_pkey PRIMARY KEY (id);


--
-- TOC entry 1847 (class 2606 OID 24676)
-- Name: user_email_key; Type: CONSTRAINT; Schema: public; Owner: testapp_role; Tablespace: 
--

ALTER TABLE ONLY accountuser
    ADD CONSTRAINT user_email_key UNIQUE (email);


--
-- TOC entry 1849 (class 2606 OID 24678)
-- Name: user_pkey; Type: CONSTRAINT; Schema: public; Owner: testapp_role; Tablespace: 
--

ALTER TABLE ONLY accountuser
    ADD CONSTRAINT user_pkey PRIMARY KEY (id);

	--
	-- Setup data for games table
	--
INSERT INTO game (title) VALUES ('League of Legends');

-- Completed on 2014-12-15 08:20:42

--
-- PostgreSQL database dump complete
--

