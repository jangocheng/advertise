package com.zhou.ad.service.impl;

import com.zhou.ad.dao.CreativeRepository;
import com.zhou.ad.entity.Creative;
import com.zhou.ad.service.ICreativeService;
import com.zhou.ad.vo.CreativeRequest;
import com.zhou.ad.vo.CreativeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CreativeServiceImpl implements ICreativeService {

    private final CreativeRepository creativeRepository;

    @Autowired
    public CreativeServiceImpl(CreativeRepository creativeRepository) {
        this.creativeRepository = creativeRepository;
    }

    @Override
    public CreativeResponse createCreative(CreativeRequest request) {

        Creative creative = creativeRepository.save(request.convertToEntity());

        return new CreativeResponse(creative.getId(), creative.getName());
    }
}
