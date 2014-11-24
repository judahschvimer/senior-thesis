import sys


def print_transition(f, action, start_state, end_state, probability):
    f.write('T : {0} : {1} : {2} {3}\n'.format(action, start_state, end_state, probability))

def print_observation(f, action, end_state, observation, probability):
    f.write('O : {0} : {1} : {2} {3}\n'.format(action, end_state, observation, probability))

def print_reward(f, action, start_state, end_state, observation, probability):
    f.write('R : {0} : {1} : {2} : {3} {4}\n'.format(action, start_state, end_state, observation, probability))

def wtp(p):
    return 'wtp-' + str(p)

def price(p):
    return 'p-' + str(p)

# Inputs: a willingness to pay and a price
# Outputs: probability of leaving given the wtp and price
def leave_function(price, wtp, leave_slope):
    if price <= wtp:
        return 0
    else:
        return leave_slope * (price - wtp)

def write_header(f, discount, values, prices, wtps):
    f.write('discount: {0}\n'.format(discount))
    f.write('values: {0}\n'.format(values))
    f.write('states: {0} done\n'.format(' '.join([wtp(p) for p in wtps])))
    f.write('actions: {0}\n'.format(' '.join([price(p) for p in prices])))
    f.write('observations: o-1 o-2\n')
    f.write('\n')

    f.write('start include: {0}\n'.format(' '.join([wtp(p) for p in wtps])))
    f.write('\n')

def write_transitions(f, prices, wtps, leave_slope):
    print_transition(f, '*', 'done', 'done', 1.0)
    f.write('\n')
    for p in prices:
        for w in wtps:
            if p <= w:
                print_transition(f, price(p), wtp(w), 'done', 1.0)
            else:
                trans_prob = leave_function(p, w, leave_slope)
                print_transition(f, price(p), wtp(w), wtp(w), 1.0 - trans_prob)
                print_transition(f, price(p), wtp(w), 'done', trans_prob)
        f.write('\n')

def write_observations(f, prices, wtps):
    print_observation(f, '*', '*', 'o-1', 1.0)
    f.write('\n')

def write_rewards(f, prices, wtps):
    for p in prices:
        for w in wtps:
            if p <= w:
                print_reward(f, price(p), wtp(w), 'done', '*', float(p))
        f.write('\n')

def write_pomdp(out_file_name, discount, num_prices, values, leave_slope):
    wtps = range(0, num_prices)
    prices = range(0, num_prices)
    with open(out_file_name, 'w') as f:
        write_header(f, discount, values, prices, wtps)
        write_transitions(f, prices, wtps, leave_slope)
        write_observations(f, prices, wtps)
        write_rewards(f, prices, wtps)

def main():
    out_file_name = 'SolveConsumer.POMDP'
    discount = 0.95
    num_prices = 5
    values = 'reward'
    leave_probability = 0.2

    write_pomdp(out_file_name, discount, num_prices, values, leave_probability)

if __name__ == '__main__':
    main()