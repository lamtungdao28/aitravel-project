package com.travelhub.ai.client;
import com.fasterxml.jackson.databind.JsonNode;
public interface LlmClient { JsonNode complete(String instructions,Object data); }
