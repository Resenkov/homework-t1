package resenkov.work.t1business.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class UnlockServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(UnlockServiceClient.class);

    private final RestTemplate restTemplate;
    private final String unlockServiceUrl;

    public UnlockServiceClient(RestTemplate restTemplate,
                               @Value("${service3.url}") String unlockServiceUrl) {
        this.restTemplate = restTemplate;
        this.unlockServiceUrl = unlockServiceUrl;
    }

    public void requestClientUnlock(List<Long> clientIds) {
        String url = unlockServiceUrl + "/api/unlock/clients";
        logger.debug("Sending client unlock request to: {}", url);
        restTemplate.postForObject(url, new IdListRequest(clientIds), Void.class);
        logger.info("Client unlock request sent for IDs: {}", clientIds);
    }

    public void requestAccountUnlock(List<Long> accountIds) {
        String url = unlockServiceUrl + "/api/unlock/accounts";
        logger.debug("Sending account unlock request to: {}", url);
        restTemplate.postForObject(url, new IdListRequest(accountIds), Void.class);
        logger.info("Account unlock request sent for IDs: {}", accountIds);
    }

    private static class IdListRequest {
        private final List<Long> ids;

        public IdListRequest(List<Long> ids) {
            this.ids = ids;
        }

        public List<Long> getIds() {
            return ids;
        }
    }
}