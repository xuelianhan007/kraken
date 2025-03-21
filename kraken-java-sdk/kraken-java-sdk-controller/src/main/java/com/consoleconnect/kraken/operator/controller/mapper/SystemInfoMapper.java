package com.consoleconnect.kraken.operator.controller.mapper;

import com.consoleconnect.kraken.operator.controller.model.SystemInfo;
import com.consoleconnect.kraken.operator.core.entity.SystemInfoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper()
public interface SystemInfoMapper {
  SystemInfoMapper INSTANCE = Mappers.getMapper(SystemInfoMapper.class);

  SystemInfo toDto(SystemInfoEntity userToken);
}
