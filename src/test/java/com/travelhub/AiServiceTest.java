package com.travelhub;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelhub.ai.client.*;
import com.travelhub.ai.dto.AiInputs.*;
import com.travelhub.ai.exception.*;
import com.travelhub.ai.service.OpenAiService;
import com.travelhub.config.AiProperties;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AiServiceTest {
    private final ObjectMapper json=new ObjectMapper();
    @Test void validatesRecommendationsAgainstBackendCandidates() throws Exception {
        LlmClient client=mock(LlmClient.class);
        OpenAiService service=new OpenAiService(client);
        var input=new RecommendationInput(List.of(),List.of(),List.of(),
                List.of(new Candidate(1L,"Lake","Hanoi",List.of("Nature"),0,BigDecimal.valueOf(4),1)));
        when(client.complete(anyString(),any())).thenReturn(json.readTree("""
                {"recommendations":[{"placeId":999,"score":0.9,"reason":"Invented"}]}
                """));
        assertThatThrownBy(() -> service.recommendPlaces(input,5)).isInstanceOf(AiResponseException.class);
        when(client.complete(anyString(),any())).thenReturn(json.readTree("""
                {"recommendations":[{"placeId":1,"score":0.9,"reason":"Matches nature interests"}]}
                """));
        assertThat(service.recommendPlaces(input,5).getFirst().name()).isEqualTo("Lake");
    }
    @Test void rejectsMalformedSummaryAndScores() throws Exception {
        LlmClient client=mock(LlmClient.class);
        OpenAiService service=new OpenAiService(client);
        when(client.complete(anyString(),any())).thenReturn(json.readTree("{\"summary\":\"Fine\"}"));
        assertThatThrownBy(() -> service.summarizeReviews(new ReviewInput(1L,"Lake",BigDecimal.ONE,1,List.of())))
                .isInstanceOf(AiResponseException.class);
        when(client.complete(anyString(),any())).thenReturn(json.readTree("""
                {"recommendations":[{"placeId":1,"score":1.1,"reason":"Invalid"}]}
                """));
        var input=new RecommendationInput(List.of(),List.of(),List.of(),List.of(new Candidate(1L,"Lake","Hanoi",List.of(),0,BigDecimal.ONE,1)));
        assertThatThrownBy(() -> service.recommendPlaces(input,1)).isInstanceOf(AiResponseException.class);
    }
    @Test void sendsCompatibleRequestAndRejectsTruncatedProviderOutput() throws Exception {
        RestClient.Builder builder=RestClient.builder().baseUrl("https://provider.example/v1");
        MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
        AiProperties p=new AiProperties();p.setEnabled(true);p.setApiKey("test-key");
        OpenAiLlmClient client=new OpenAiLlmClient(builder.build(),json,p);
        server.expect(requestTo("https://provider.example/v1/chat/completions"))
                .andExpect(header("Authorization","Bearer test-key"))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andRespond(withSuccess("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{\\\"summary\\\":\\\"Good\\\"}\"}}]}",MediaType.APPLICATION_JSON));
        assertThat(client.complete("Return JSON",Map.of("reviews",List.of())).path("summary").asText()).isEqualTo("Good");
        server.verify();server.reset();
        server.expect(requestTo("https://provider.example/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"{}\"}}]}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.complete("Return JSON",Map.of())).isInstanceOf(AiResponseException.class);
        server.verify();
    }
    @Test void disabledProviderNeverSendsARequest() {
        AiProperties p=new AiProperties();
        var client=new OpenAiLlmClient(RestClient.create(),json,p);
        assertThatThrownBy(() -> client.complete("JSON",Map.of())).isInstanceOf(AiUnavailableException.class);
    }
}
