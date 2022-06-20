import numpy as np

def sparse_label_converter(label):
    label_num = np.max(label) + 1
    result = np.zeros((len(label), label_num))
    for i, l in enumerate(label):
        result[i, l] = 1
    return result


def balance_dataset(label, data, min_thres, negative_amp=1, sample_seed=None):
    # obtain number of each label
    if sample_seed is not None:
        np.random.seed(sample_seed)
    count = [np.sum(label==i) for i in range(max(label) + 1)]
    sample_size = max(min_thres, min(count))
    new_data_set = []
    new_label_set = []
    for i in range(max(label) + 1):
        tmp = data[label == i]
        size = sample_size if i > 0 else sample_size * negative_amp
        if tmp.shape[0] > size:
            ptmp = np.random.permutation(tmp)[:size]
            new_data_set.append(ptmp)
            new_label_set.append(np.ones(size, dtype=int) * i)
        else:
            new_data_set.append(tmp)
            new_label_set.append(np.ones(tmp.shape[0], dtype=int) * i)
    return np.concatenate(new_label_set), np.concatenate(new_data_set)