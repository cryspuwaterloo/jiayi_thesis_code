import numpy as np

DEFAULT_SUPPORTED_AUTH_TYPES = ['gait', 'touch']

SCHEME_CROSS_SESSION = 'cross_session'
SCHEME_TIME_SEQ_10 = 'time_10'
SCHEME_TIME_SEQ_20 = 'time_20'

SUPPORTED_SPLITTING_SCHEMES = [
    SCHEME_CROSS_SESSION,
    SCHEME_TIME_SEQ_10,
    SCHEME_TIME_SEQ_20,
]



class BiometricStorage:
    IMPOSTER_NAME = -1

    def add_session_data(self, user, auth, data):
        self.data[user][auth].append(np.array(data))

    def split_train_test_cross_session(self, user, auth, test_no=None):
        n_sessions = self.count_session_num(user, auth)

        if n_sessions == 1:
            return self.data[user][auth][0], np.array([])

        test_session = n_sessions - 1 if test_no is None else test_no
        train_set = np.concatenate([
            data for i, data in enumerate(self.data[user][auth])
            if i != test_session
        ])
        test_set = self.data[user][auth][test_session]
        return train_set, test_set

    def split_train_test_time_seq(self, user, auth, percent):
        all_data = np.concatenate([
            data for i, data in enumerate(self.data[user][auth])
            if len(data) > 0
        ])
        test_size = int(all_data.shape[0] * percent)
        test_set = all_data[-test_size:]
        train_set = all_data[:-test_size]
        return train_set, test_set

    def get_train_test_sets(self, auth, scheme=SCHEME_TIME_SEQ_10):
        assert scheme in SUPPORTED_SPLITTING_SCHEMES
        result = {
            'train': {}, 'test': {}
        }
        for user in self.users:
            if scheme == SCHEME_CROSS_SESSION:
                train_, test_ = self.split_train_test_cross_session(user, auth)
                result['train'][user] = train_
                result['test'][user] = test_
            elif scheme == SCHEME_TIME_SEQ_10:
                train_, test_ = self.split_train_test_time_seq(user, auth, 0.1)
                result['train'][user] = train_
                result['test'][user] = test_
            elif scheme == SCHEME_TIME_SEQ_20:
                train_, test_ = self.split_train_test_time_seq(user, auth, 0.1)
                result['train'][user] = train_
                result['test'][user] = test_


        result['train'][self.IMPOSTER_NAME] = self.imposters[auth]['train']
        result['test'][self.IMPOSTER_NAME] = self.imposters[auth]['test']

        return result

    def initialize_storage(self):
        for user in self.users:
            self.data[user] = {
                auth: [] for auth in self.auth_types
            }

    def count_session_num(self, user, auth):
        return len(self.data[user][auth])

    def add_user(self, user):

        self.users.append(user)
        self.data[user] = {
            auth: [] for auth in self.auth_types
        }

    def __init__(self, users=None, auth_types=DEFAULT_SUPPORTED_AUTH_TYPES,
                 imposter_set=None):
        self.users = [] if users is None else users
        self.auth_types = auth_types
        self.data = dict()
        self.imposters = imposter_set
