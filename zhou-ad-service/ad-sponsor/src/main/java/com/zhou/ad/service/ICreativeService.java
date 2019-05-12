package com.zhou.ad.service;

import com.zhou.ad.vo.CreativeRequest;
import com.zhou.ad.vo.CreativeResponse;

public interface ICreativeService {

    CreativeResponse createCreative(CreativeRequest request);
}
