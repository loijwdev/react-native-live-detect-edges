declare module 'react-native/Libraries/Types/CodegenTypes' {
  import type { NativeSyntheticEvent } from 'react-native';

  export type Double = number;
  export type Float = number;
  export type Int32 = number;
  export type DefaultTypes = number | boolean | string | ReadonlyArray<string>;

  export type DirectEventHandler<T> = (
    event: NativeSyntheticEvent<T>
  ) => void | Promise<void>;

  export type BubblingEventHandler<T> = (
    event: NativeSyntheticEvent<T>
  ) => void | Promise<void>;

  export type WithDefault<
    Type extends DefaultTypes,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    _Value extends Type | string | undefined | null
  > = Type | undefined | null;
}
