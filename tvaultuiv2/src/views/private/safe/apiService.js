import { mockApi, mockCreateSafe } from '../../../services';

const fetchSafe = (data) => mockApi(data);
const createSafe = (data) => mockCreateSafe(data);
const searchUser = (data) => mockApi(data);
export default { fetchSafe, createSafe, searchUser };
