import { mockApi, mockCreateSafe } from '../../../services';

const fetchSafe = (data) => mockApi(data);
const createSafe = (data) => mockCreateSafe(data);

export default { fetchSafe, createSafe };
