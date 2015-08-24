
DROP TABLE JOBSTATUS;

DROP TABLE STEPSTATUS;

DROP TABLE CHECKPOINTDATA;

DROP TABLE JOBINSTANCEDATA;

DROP TABLE EXECUTIONINSTANCEDATA;

DROP TABLE STEPEXECUTIONINSTANCEDATA;

CREATE TABLE JOBINSTANCEDATA(
  jobinstanceid   BIGINT NOT NULL PRIMARY KEY IDENTITY,
  name    VARCHAR(512), 
  apptag VARCHAR(512)
);

CREATE TABLE EXECUTIONINSTANCEDATA(
  jobexecid  BIGINT NOT NULL PRIMARY KEY IDENTITY, 
  jobinstanceid BIGINT,
  createtime  DATETIME,
  starttime   DATETIME,
  endtime   DATETIME,
  updatetime  DATETIME,
  parameters  VARBINARY,
  batchstatus   VARCHAR(512),
  exitstatus    VARCHAR(512),
  CONSTRAINT JOBINST_JOBEXEC_FK FOREIGN KEY (jobinstanceid) REFERENCES JOBINSTANCEDATA (jobinstanceid)
);

CREATE TABLE STEPEXECUTIONINSTANCEDATA(
  stepexecid BIGINT NOT NULL PRIMARY KEY IDENTITY, 
  jobexecid BIGINT,
  batchstatus         VARCHAR(512),
    exitstatus      VARCHAR(512),
    stepname      VARCHAR(512),
  readcount       INTEGER,
  writecount      INTEGER,
  commitcount     INTEGER,
  rollbackcount   INTEGER,
  readskipcount   INTEGER,
  processskipcount  INTEGER,
  filtercount       INTEGER,
  writeskipcount    INTEGER,
  startTime           DATETIME,
  endTime             DATETIME,
  persistentData    VARBINARY,
  CONSTRAINT JOBEXEC_STEPEXEC_FK FOREIGN KEY (jobexecid) REFERENCES EXECUTIONINSTANCEDATA (jobexecid)
);  

CREATE TABLE JOBSTATUS (
  id		BIGINT NOT NULL PRIMARY KEY,
  obj		VARBINARY,
  CONSTRAINT JOBSTATUS_JOBINST_FK FOREIGN KEY (id) REFERENCES JOBINSTANCEDATA (jobinstanceid) ON DELETE CASCADE
);

CREATE TABLE STEPSTATUS(
  id		BIGINT NOT NULL PRIMARY KEY,
  obj		VARBINARY,
  CONSTRAINT STEPSTATUS_STEPEXEC_FK FOREIGN KEY (id) REFERENCES STEPEXECUTIONINSTANCEDATA (stepexecid) ON DELETE CASCADE
);

CREATE TABLE CHECKPOINTDATA(
  id		VARCHAR(512),
  obj		VARBINARY
);

  
