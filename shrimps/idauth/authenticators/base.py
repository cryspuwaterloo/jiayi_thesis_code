from abc import ABC, abstractmethod

class Authenticator(ABC):
    """
    Biometrics is a base class for all behavioral biometrics based authenticator

    There are three derived biometric-based authenticators:

    - event-driven
    - sampling-based
    - one-time

    """


    @abstractmethod
    def extract_features(self, raw_data):
        """
        extract features from raw data
        """
        pass

    @abstractmethod
    def train_model(self, train_data, test_data=None):
        """
        train_data: list of (label , features)
        test_data: list of (label, features)
        """
        pass

    @abstractmethod
    def authenticate(self, features):
        """
        input should
        """
        pass

