import { mockApi } from '../../../services';

const fetchSafe = (data) => mockApi(data);

const searchUser = (data) => mockApi(data);
export default { fetchSafe, searchUser };
