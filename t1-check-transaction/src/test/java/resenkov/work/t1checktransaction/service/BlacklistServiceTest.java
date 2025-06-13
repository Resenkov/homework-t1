package resenkov.work.t1checktransaction.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import resenkov.work.t1checktransaction.repository.BlacklistRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlacklistServiceTest {

    @Mock
    private BlacklistRepository blacklistRepository;

    @InjectMocks
    private BlacklistService service;

    @Test
    void isClientInBlacklist_returnsTrueWhenExists() {
        Long clientId = 123L;
        when(blacklistRepository.existsByClientId(clientId)).thenReturn(true);

        boolean result = service.isClientInBlacklist(clientId);

        assertThat(result).isTrue();
        verify(blacklistRepository).existsByClientId(clientId);
    }

    @Test
    void isClientInBlacklist_returnsFalseWhenNotExists() {
        Long clientId = 456L;
        when(blacklistRepository.existsByClientId(clientId)).thenReturn(false);

        boolean result = service.isClientInBlacklist(clientId);

        assertThat(result).isFalse();
        verify(blacklistRepository).existsByClientId(clientId);
    }
}
