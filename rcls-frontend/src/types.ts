type Brand<T, B> = T & { __brand: B };

export type LocalLink = Brand<string, "LocalLink">