import numpy as np

class ImposterProvider:
    """
    provide negative training and testing data for authenticators
    """

    def generate_train_test_sets(self, n_test,
                                 shuffle=False, random_state=None):
        assert n_test < self.n_users
        train_sets = []
        test_sets = []
        seq = list(range(self.n_users))
        if shuffle:
            np.random.seed(random_state)
            seq = np.random.permutation(list(range(self.n_users)))

        for i in seq[:-n_test]:
            train_sets.append(self.data[i])
        for i in seq[-n_test:]:
            test_sets.append(self.data[i])

        return np.concatenate(train_sets), np.concatenate(test_sets)

    def __init__(self, dataset):
        self.data = dataset
        self.n_users = len(dataset)