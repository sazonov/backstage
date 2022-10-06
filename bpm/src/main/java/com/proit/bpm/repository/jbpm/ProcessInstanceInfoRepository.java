package com.proit.bpm.repository.jbpm;

import com.proit.app.repository.generic.CustomJpaRepository;
import org.jbpm.persistence.processinstance.ProcessInstanceInfo;

public interface ProcessInstanceInfoRepository extends CustomJpaRepository<ProcessInstanceInfo, Long>
{
}
